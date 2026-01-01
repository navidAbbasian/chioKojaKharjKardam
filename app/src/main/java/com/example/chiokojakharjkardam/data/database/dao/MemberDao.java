package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Member;

import java.util.List;

@Dao
public interface MemberDao {

    @Insert
    long insert(Member member);

    @Update
    void update(Member member);

    @Delete
    void delete(Member member);

    @Query("SELECT * FROM members WHERE familyId = :familyId ORDER BY isOwner DESC, createdAt ASC")
    LiveData<List<Member>> getMembersByFamily(long familyId);

    @Query("SELECT * FROM members ORDER BY isOwner DESC, createdAt ASC")
    LiveData<List<Member>> getAllMembers();

    @Query("SELECT * FROM members WHERE id = :id")
    LiveData<Member> getMemberById(long id);

    @Query("SELECT * FROM members WHERE id = :id")
    Member getMemberByIdSync(long id);

    @Query("SELECT * FROM members WHERE isOwner = 1 LIMIT 1")
    Member getOwnerSync();

    @Query("SELECT COUNT(*) FROM members")
    int getMemberCount();
}

