package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.MemberDao;
import com.example.chiokojakharjkardam.data.database.entity.Member;

import java.util.List;

public class MemberRepository {

    private final MemberDao memberDao;

    public MemberRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        memberDao = db.memberDao();
    }

    public void insert(Member member) {
        AppDatabase.databaseWriteExecutor.execute(() -> memberDao.insert(member));
    }

    public void insertAndGetId(Member member, OnMemberInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = memberDao.insert(member);
            if (listener != null) {
                listener.onMemberInserted(id);
            }
        });
    }

    public void update(Member member) {
        AppDatabase.databaseWriteExecutor.execute(() -> memberDao.update(member));
    }

    public void delete(Member member) {
        AppDatabase.databaseWriteExecutor.execute(() -> memberDao.delete(member));
    }

    public LiveData<List<Member>> getAllMembers() {
        return memberDao.getAllMembers();
    }

    public LiveData<List<Member>> getMembersByFamily(long familyId) {
        return memberDao.getMembersByFamily(familyId);
    }

    public LiveData<Member> getMemberById(long id) {
        return memberDao.getMemberById(id);
    }

    public interface OnMemberInsertedListener {
        void onMemberInserted(long memberId);
    }
}

