package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Member;

import java.util.List;

@Dao
public interface MemberDao {

    @Insert
    long insert(Member member);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Member member);

    @Update
    void update(Member member);

    @Delete
    void delete(Member member);

    @Query("DELETE FROM members")
    void deleteAll();

    @Query("SELECT * FROM members WHERE familyId = :familyId ORDER BY isOwner DESC, createdAt ASC")
    LiveData<List<Member>> getMembersByFamily(long familyId);

    @Query("SELECT * FROM members ORDER BY isOwner DESC, createdAt ASC")
    LiveData<List<Member>> getAllMembers();

    @Query("SELECT * FROM members ORDER BY isOwner DESC, createdAt ASC")
    List<Member> getAllMembersSync();

    @Query("SELECT * FROM members WHERE id = :id")
    LiveData<Member> getMemberById(long id);

    @Query("SELECT * FROM members WHERE id = :id")
    Member getMemberByIdSync(long id);

    @Query("SELECT * FROM members WHERE isOwner = 1 LIMIT 1")
    Member getOwnerSync();

    @Query("SELECT COUNT(*) FROM members")
    int getMemberCount();

    @Query("UPDATE members SET isOwner = 0")
    void clearAllOwners();

    @Query("UPDATE members SET isOwner = 1 WHERE id = :memberId")
    void setOwner(long memberId);

    @Query("SELECT * FROM members WHERE userId = :userId LIMIT 1")
    Member getByUserIdSync(String userId);

    @Query("DELETE FROM members WHERE userId NOT IN (:userIds)")
    void deleteObsoleteMembers(List<String> userIds);
}

