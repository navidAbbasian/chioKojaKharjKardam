package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tombstone table: records the Supabase IDs of items deleted locally while offline.
 * SyncManager processes this table during the upload phase and deletes from Supabase,
 * then removes the tombstone.
 */
@Entity(tableName = "pending_deletes")
public class PendingDelete {

    public static final String TYPE_TRANSACTION = "transaction";
    public static final String TYPE_BANK_CARD   = "bank_card";
    public static final String TYPE_CATEGORY    = "category";
    public static final String TYPE_TAG         = "tag";

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** One of the TYPE_* constants above. */
    private String entityType;

    /** The Supabase (cloud) ID of the deleted record. String to support both long and UUID IDs. */
    private String supabaseId;

    public PendingDelete() {}

    public PendingDelete(String entityType, String supabaseId) {
        this.entityType = entityType;
        this.supabaseId = supabaseId;
    }

    /** Convenience constructor for entities with long Supabase IDs (bank_card, category, tag). */
    public PendingDelete(String entityType, long supabaseId) {
        this(entityType, String.valueOf(supabaseId));
    }

    public int getId()            { return id; }
    public void setId(int id)     { this.id = id; }

    public String getEntityType()              { return entityType; }
    public void setEntityType(String t)        { this.entityType = t; }

    public String getSupabaseId()                  { return supabaseId; }
    public void setSupabaseId(String supabaseId)   { this.supabaseId = supabaseId; }
}
