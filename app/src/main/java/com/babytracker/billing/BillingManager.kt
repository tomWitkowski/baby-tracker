package com.babytracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.babytracker.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play Billing for the Pro subscription.
 *
 * Product IDs must be configured in Google Play Console before they work.
 * Replace [PRODUCT_ID_PRO_MONTHLY] / [PRODUCT_ID_PRO_YEARLY] with the
 * actual IDs you create in Play Console → Monetise → Subscriptions.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {

    companion object {
        private const val TAG = "BillingManager"

        // TODO: Replace with your actual subscription product IDs from Play Console
        const val PRODUCT_ID_PRO_MONTHLY = "pro_monthly"
        const val PRODUCT_ID_PRO_YEARLY = "pro_yearly"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPro = MutableStateFlow(appPreferences.isPro)
    val isPro: StateFlow<Boolean> = _isPro

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        } else {
            Log.w(TAG, "Purchases update failed: ${result.debugMessage}")
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connectAndRefresh()
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private fun connectAndRefresh() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        refreshPurchases()
                        loadProductDetails()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected — will retry on next launch")
            }
        })
    }

    // ── Purchase state ────────────────────────────────────────────────────────

    /** Query Play Store for currently active purchases and update local state. */
    suspend fun refreshPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(result.purchasesList)
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val hasActiveSub = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it == PRODUCT_ID_PRO_MONTHLY || it == PRODUCT_ID_PRO_YEARLY }
        }

        // Acknowledge unacknowledged purchases
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledgePurchase(it) }

        appPreferences.isPro = hasActiveSub
        _isPro.value = hasActiveSub
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = billingClient.acknowledgePurchase(params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
        }
    }

    // ── Product details ───────────────────────────────────────────────────────

    private suspend fun loadProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList ?: emptyList()
        } else {
            Log.e(TAG, "Load product details failed: ${result.billingResult.debugMessage}")
        }
    }

    // ── Purchase flow ─────────────────────────────────────────────────────────

    /**
     * Launch the Play Store subscription purchase sheet.
     * @param activity  Calling Activity (required by Billing API)
     * @param productDetails  Obtained from [productDetails] StateFlow
     * @param offerToken  Offer token from [ProductDetails.SubscriptionOfferDetails];
     *                    pass the first available one if you have a single plan.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient not ready")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}
