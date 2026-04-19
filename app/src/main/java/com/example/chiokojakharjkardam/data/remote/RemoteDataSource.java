package com.example.chiokojakharjkardam.data.remote;

import android.util.Log;

import com.example.chiokojakharjkardam.data.remote.model.*;
import com.example.chiokojakharjkardam.utils.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * High-level Supabase operations used by repositories and SyncManager.
 *
 * All filter params follow PostgREST convention: "eq.VALUE".
 * Use SessionManager.familyFilter() / userFilter() helpers.
 */
public class RemoteDataSource {

    private static final String TAG = "RemoteDataSource";
    private static volatile RemoteDataSource instance;

    private final SupabaseRestService rest;
    private final SupabaseAuthService auth;

    private RemoteDataSource() {
        rest = SupabaseClient.getInstance().rest();
        auth = SupabaseClient.getInstance().auth();
    }

    public static RemoteDataSource getInstance() {
        if (instance == null) {
            synchronized (RemoteDataSource.class) {
                if (instance == null) instance = new RemoteDataSource();
            }
        }
        return instance;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    // ──────────────────────────────────────────────────────────────
    // FAMILIES
    // ──────────────────────────────────────────────────────────────

    public void createFamily(RemoteFamily family, Callback<RemoteFamily> cb) {
        rest.createFamily(family).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void getFamilyByInviteCode(String code, Callback<RemoteFamily> cb) {
        Map<String, String> body = new HashMap<>();
        body.put("p_code", code);
        rest.getFamilyByInviteCode(body)
                .enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void getFamilyById(String familyId, Callback<RemoteFamily> cb) {
        rest.getFamilyById("eq." + familyId, "*")
                .enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    // ──────────────────────────────────────────────────────────────
    // PROFILES
    // ──────────────────────────────────────────────────────────────

    public void getProfile(String userId, Callback<RemoteProfile> cb) {
        rest.getProfile("eq." + userId, "*")
                .enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void getProfilesByFamily(String familyId, Callback<List<RemoteProfile>> cb) {
        rest.getProfilesByFamily("eq." + familyId, "*").enqueue(wrapList(cb));
    }

    /** Upsert (insert-or-update) the current user's profile row. */
    public void upsertProfile(RemoteProfile profile, Callback<RemoteProfile> cb) {
        rest.upsertProfile(profile)
                .enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateProfile(String userId, Map<String, Object> update, Callback<Void> cb) {
        rest.updateProfile("eq." + userId, update).enqueue(new retrofit2.Callback<List<RemoteProfile>>() {
            @Override public void onResponse(Call<List<RemoteProfile>> call, Response<List<RemoteProfile>> r) {
                if (r.isSuccessful()) cb.onSuccess(null); else cb.onError(errorMsg(r));
            }
            @Override public void onFailure(Call<List<RemoteProfile>> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // BANK CARDS
    // ──────────────────────────────────────────────────────────────

    public void getBankCards(String familyId, Callback<List<RemoteBankCard>> cb) {
        rest.getBankCards("eq." + familyId, "created_at.asc").enqueue(wrapList(cb));
    }

    public void insertBankCard(RemoteBankCard card, Callback<RemoteBankCard> cb) {
        rest.insertBankCard(card).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateBankCard(long id, Map<String, Object> update, Callback<Void> cb) {
        rest.updateBankCard("eq." + id, update).enqueue(ignoreBody(cb));
    }

    public void deleteBankCard(long id, Callback<Void> cb) {
        rest.deleteBankCard("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // CATEGORIES
    // ──────────────────────────────────────────────────────────────

    public void getCategories(String familyId, Callback<List<RemoteCategory>> cb) {
        rest.getCategories("eq." + familyId, "name.asc").enqueue(wrapList(cb));
    }

    public void insertCategory(RemoteCategory cat, Callback<RemoteCategory> cb) {
        rest.insertCategory(cat).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateCategory(long id, Map<String, Object> update, Callback<Void> cb) {
        rest.updateCategory("eq." + id, update).enqueue(ignoreBody(cb));
    }

    public void deleteCategory(long id, Callback<Void> cb) {
        rest.deleteCategory("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // TAGS
    // ──────────────────────────────────────────────────────────────

    public void getTags(String familyId, Callback<List<RemoteTag>> cb) {
        rest.getTags("eq." + familyId, "name.asc").enqueue(wrapList(cb));
    }

    public void insertTag(RemoteTag tag, Callback<RemoteTag> cb) {
        rest.insertTag(tag).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateTag(long id, Map<String, Object> update, Callback<Void> cb) {
        rest.updateTag("eq." + id, update).enqueue(ignoreBody(cb));
    }

    public void deleteTag(long id, Callback<Void> cb) {
        rest.deleteTag("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // TRANSACTIONS
    // ──────────────────────────────────────────────────────────────

    public void getTransactions(String familyId, Callback<List<RemoteTransaction>> cb) {
        rest.getTransactions("eq." + familyId, "date.desc").enqueue(wrapList(cb));
    }

    public void insertTransaction(RemoteTransaction tx, Callback<RemoteTransaction> cb) {
        rest.insertTransaction(tx).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateTransaction(String id, Map<String, Object> update, Callback<Void> cb) {
        rest.updateTransaction("eq." + id, update).enqueue(ignoreBody(cb));
    }

    public void deleteTransaction(String id, Callback<Void> cb) {
        rest.deleteTransaction("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // TRANSACTION TAGS
    // ──────────────────────────────────────────────────────────────

    public void insertTransactionTag(String txId, long tagId, Callback<Void> cb) {
        rest.insertTransactionTag(new RemoteTransactionTag(txId, tagId)).enqueue(voidCb(cb));
    }

    public void deleteTransactionTags(String txId, Callback<Void> cb) {
        rest.deleteTransactionTags("eq." + txId).enqueue(voidCb(cb));
    }

    public void getTransactionTags(String txId, Callback<List<RemoteTransactionTag>> cb) {
        rest.getTransactionTags("eq." + txId).enqueue(wrapList(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // BILLS
    // ──────────────────────────────────────────────────────────────

    public void getBills(String familyId, Callback<List<RemoteBill>> cb) {
        rest.getBills("eq." + familyId, "due_date.asc").enqueue(wrapList(cb));
    }

    public void insertBill(RemoteBill bill, Callback<RemoteBill> cb) {
        rest.insertBill(bill).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void updateBill(long id, Map<String, Object> update, Callback<Void> cb) {
        rest.updateBill("eq." + id, update).enqueue(ignoreBody(cb));
    }

    public void deleteBill(long id, Callback<Void> cb) {
        rest.deleteBill("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // TRANSFERS
    // ──────────────────────────────────────────────────────────────

    public void getTransfers(String familyId, Callback<List<RemoteTransfer>> cb) {
        rest.getTransfers("eq." + familyId, "date.desc").enqueue(wrapList(cb));
    }

    public void insertTransfer(RemoteTransfer transfer, Callback<RemoteTransfer> cb) {
        rest.insertTransfer(transfer).enqueue(wrap(cb, list -> list.isEmpty() ? null : list.get(0)));
    }

    public void deleteTransfer(long id, Callback<Void> cb) {
        rest.deleteTransfer("eq." + id).enqueue(voidCb(cb));
    }

    // ──────────────────────────────────────────────────────────────
    // MEMBER REMOVAL (owner only — sets family_id to null)
    // ──────────────────────────────────────────────────────────────

    public void removeMemberFromFamily(String userId, Callback<Void> cb) {
        Map<String, Object> update = new HashMap<>();
        update.put("family_id", null);
        update.put("is_owner", false);
        updateProfile(userId, update, cb);
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    interface Mapper<T, R> { R map(T input); }

    private <T, R> retrofit2.Callback<List<T>> wrap(Callback<R> cb, Mapper<List<T>, R> mapper) {
        return new retrofit2.Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(mapper.map(r.body()));
                else cb.onError(errorMsg(r));
            }
            @Override public void onFailure(Call<List<T>> call, Throwable t) {
                Log.e(TAG, "Request failed: " + t.getMessage());
                cb.onError(t.getMessage());
            }
        };
    }

    private <T> retrofit2.Callback<List<T>> wrapList(Callback<List<T>> cb) {
        return new retrofit2.Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError(errorMsg(r));
            }
            @Override public void onFailure(Call<List<T>> call, Throwable t) {
                Log.e(TAG, "Request failed: " + t.getMessage());
                cb.onError(t.getMessage());
            }
        };
    }

    private <T> retrofit2.Callback<List<T>> ignoreBody(Callback<Void> cb) {
        return new retrofit2.Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> r) {
                if (r.isSuccessful()) cb.onSuccess(null); else cb.onError(errorMsg(r));
            }
            @Override public void onFailure(Call<List<T>> call, Throwable t) { cb.onError(t.getMessage()); }
        };
    }

    private retrofit2.Callback<Void> voidCb(Callback<Void> cb) {
        return new retrofit2.Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null); else cb.onError(errorMsg(r));
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
        };
    }

    private <T> String errorMsg(Response<T> r) {
        try {
            String body = r.errorBody() != null ? r.errorBody().string() : "";
            return "HTTP " + r.code() + ": " + body;
        } catch (Exception e) { return "HTTP " + r.code(); }
    }
}


