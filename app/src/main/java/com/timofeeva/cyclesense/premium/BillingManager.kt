package com.timofeeva.cyclesense.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManager(
    context: Context,
    private val premiumAccessRepository: PremiumAccessRepository,
    private val onPremiumChanged: (Boolean) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {
    private val appContext = context.applicationContext

    private var premiumProductDetails: ProductDetails? = null
    private var premiumOfferToken: String? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener { billingResult, purchases ->
            handlePurchaseUpdate(
                billingResult = billingResult,
                purchases = purchases
            )
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryPremiumProduct()
            queryActiveSubscriptions()
            return
        }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onStatusChanged("")
                        queryPremiumProduct()
                        queryActiveSubscriptions()
                    } else {
                        onStatusChanged("Google Play сейчас недоступен. Повторите позже.")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    onStatusChanged("Google Play Billing отключён. Повторите позже.")
                }
            }
        )
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun queryPremiumProduct() {
        if (!billingClient.isReady) {
            onStatusChanged("Google Play Billing ещё не подключён")
            return
        }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PREMIUM_MONTHLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onStatusChanged("Не удалось загрузить подписку. Повторите позже.")
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.firstOrNull { productDetails ->
                productDetails.productId == PRODUCT_ID_PREMIUM_MONTHLY
            }

            if (productDetails == null) {
                onStatusChanged("")
                return@queryProductDetailsAsync
            }

            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken

            if (offerToken.isNullOrBlank()) {
                onStatusChanged("Подписка сейчас недоступна. Повторите позже.")
                return@queryProductDetailsAsync
            }

            premiumProductDetails = productDetails
            premiumOfferToken = offerToken

            val price = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice

            onStatusChanged(
                if (price != null) {
                    "Подписка загружена: $price"
                } else {
                    "Подписка загружена"
                }
            )
        }
    }

    fun queryActiveSubscriptions() {
        if (!billingClient.isReady) {
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return@queryPurchasesAsync
            }

            val hasPremium = purchases.any { purchase ->
                purchase.products.contains(PRODUCT_ID_PREMIUM_MONTHLY) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            premiumAccessRepository.setPremiumActive(hasPremium)
            onPremiumChanged(hasPremium)

            purchases.forEach { purchase ->
                if (
                    purchase.products.contains(PRODUCT_ID_PREMIUM_MONTHLY) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    acknowledgeIfNeeded(purchase)
                }
            }
        }
    }

    fun launchPremiumPurchase(activity: Activity) {
        if (!billingClient.isReady) {
            onStatusChanged("Google Play Billing ещё не подключён")
            startConnection()
            return
        }

        val productDetails = premiumProductDetails
        val offerToken = premiumOfferToken

        if (productDetails == null || offerToken.isNullOrBlank()) {
            onStatusChanged("Подписка ещё не загружена. Повторите через несколько секунд.")
            queryPremiumProduct()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onStatusChanged("Не удалось открыть оплату. Повторите позже.")
        }
    }

    private fun handlePurchaseUpdate(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { purchase ->
                    handlePurchase(purchase)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onStatusChanged("Покупка отменена")
            }

            else -> {
                onStatusChanged("Ошибка покупки. Повторите позже.")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val isPremiumPurchase = purchase.products.contains(PRODUCT_ID_PREMIUM_MONTHLY)

        if (!isPremiumPurchase) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                premiumAccessRepository.setPremiumActive(true)
                onPremiumChanged(true)
                onStatusChanged("Premium активирован")

                acknowledgeIfNeeded(purchase)
            }

            Purchase.PurchaseState.PENDING -> {
                onStatusChanged("Покупка ожидает подтверждения")
            }

            else -> {
                onStatusChanged("Покупка не завершена")
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    onStatusChanged("Premium активирован, но подтверждение покупки не завершено.")
                }
            }
        }
    }

    companion object {
        const val PRODUCT_ID_PREMIUM_MONTHLY = "cyclesense_premium_monthly"
    }
}