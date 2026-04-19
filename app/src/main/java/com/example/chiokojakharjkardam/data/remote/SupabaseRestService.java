package com.example.chiokojakharjkardam.data.remote;

import com.example.chiokojakharjkardam.data.remote.model.*;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Supabase PostgREST (REST v1) endpoints.
 *
 * Filter syntax: pass column filter as query param with "eq." prefix.
 * Example: @Query("family_id") String filter → pass "eq.UUID"
 * Prefer: return=representation → response body contains created/updated record.
 */
public interface SupabaseRestService {

    // ──────────── PROFILES ────────────

    @GET("rest/v1/profiles")
    Call<List<RemoteProfile>> getProfilesByFamily(
            @Query("family_id") String familyIdFilter,   // "eq.UUID"
            @Query("select") String select);

    @GET("rest/v1/profiles")
    Call<List<RemoteProfile>> getProfile(
            @Query("id") String idFilter,                // "eq.UUID"
            @Query("select") String select);

    /** INSERT or UPDATE profile — safe even when the trigger row doesn't exist yet */
    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/profiles")
    Call<List<RemoteProfile>> upsertProfile(@Body RemoteProfile profile);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/profiles")
    Call<List<RemoteProfile>> updateProfile(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    // ──────────── FAMILIES ────────────

    @Headers("Prefer: return=representation")
    @POST("rest/v1/families")
    Call<List<RemoteFamily>> createFamily(@Body RemoteFamily family);

    /**
     * Lookup a family by its invite code via a SECURITY DEFINER RPC function.
     * Direct GET on /families is blocked by RLS for users without a family_id yet.
     */
    @POST("rest/v1/rpc/lookup_family_by_invite_code")
    Call<List<RemoteFamily>> getFamilyByInviteCode(@Body java.util.Map<String, String> params);

    @GET("rest/v1/families")
    Call<List<RemoteFamily>> getFamilyById(
            @Query("id") String idFilter,
            @Query("select") String select);

    // ──────────── BANK CARDS ────────────

    @GET("rest/v1/bank_cards")
    Call<List<RemoteBankCard>> getBankCards(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/bank_cards")
    Call<List<RemoteBankCard>> insertBankCard(@Body RemoteBankCard card);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bank_cards")
    Call<List<RemoteBankCard>> updateBankCard(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    @DELETE("rest/v1/bank_cards")
    Call<Void> deleteBankCard(@Query("id") String idFilter);

    // ──────────── CATEGORIES ────────────

    @GET("rest/v1/categories")
    Call<List<RemoteCategory>> getCategories(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/categories")
    Call<List<RemoteCategory>> insertCategory(@Body RemoteCategory category);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/categories")
    Call<List<RemoteCategory>> updateCategory(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    @DELETE("rest/v1/categories")
    Call<Void> deleteCategory(@Query("id") String idFilter);

    // ──────────── TAGS ────────────

    @GET("rest/v1/tags")
    Call<List<RemoteTag>> getTags(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/tags")
    Call<List<RemoteTag>> insertTag(@Body RemoteTag tag);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/tags")
    Call<List<RemoteTag>> updateTag(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    @DELETE("rest/v1/tags")
    Call<Void> deleteTag(@Query("id") String idFilter);

    // ──────────── TRANSACTIONS ────────────

    @GET("rest/v1/transactions")
    Call<List<RemoteTransaction>> getTransactions(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/transactions")
    Call<List<RemoteTransaction>> insertTransaction(@Body RemoteTransaction transaction);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/transactions")
    Call<List<RemoteTransaction>> updateTransaction(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    @DELETE("rest/v1/transactions")
    Call<Void> deleteTransaction(@Query("id") String idFilter);

    // ──────────── TRANSACTION TAGS ────────────

    @GET("rest/v1/transaction_tags")
    Call<List<RemoteTransactionTag>> getTransactionTags(
            @Query("transaction_id") String transactionIdFilter);

    @POST("rest/v1/transaction_tags")
    Call<Void> insertTransactionTag(@Body RemoteTransactionTag tag);

    @DELETE("rest/v1/transaction_tags")
    Call<Void> deleteTransactionTags(
            @Query("transaction_id") String transactionIdFilter);

    // ──────────── BILLS ────────────

    @GET("rest/v1/bills")
    Call<List<RemoteBill>> getBills(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/bills")
    Call<List<RemoteBill>> insertBill(@Body RemoteBill bill);

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bills")
    Call<List<RemoteBill>> updateBill(
            @Query("id") String idFilter,
            @Body Map<String, Object> update);

    @DELETE("rest/v1/bills")
    Call<Void> deleteBill(@Query("id") String idFilter);

    // ──────────── TRANSFERS ────────────

    @GET("rest/v1/transfers")
    Call<List<RemoteTransfer>> getTransfers(
            @Query("family_id") String familyIdFilter,
            @Query("order") String order);

    @Headers("Prefer: return=representation")
    @POST("rest/v1/transfers")
    Call<List<RemoteTransfer>> insertTransfer(@Body RemoteTransfer transfer);

    @DELETE("rest/v1/transfers")
    Call<Void> deleteTransfer(@Query("id") String idFilter);
}

