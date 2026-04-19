package com.example.chiokojakharjkardam.ui.members;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.repository.FamilyRepository;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;
import com.example.chiokojakharjkardam.utils.SessionManager;

import java.util.List;

public class MembersViewModel extends AndroidViewModel {

    private final MemberRepository memberRepository;
    private final FamilyRepository familyRepository;
    private final LiveData<List<Member>> allMembers;
    private final LiveData<Family> family;

    public MembersViewModel(@NonNull Application application) {
        super(application);
        memberRepository = new MemberRepository(application);
        familyRepository = new FamilyRepository(application);
        allMembers = memberRepository.getAllMembers();
        family = familyRepository.getFirstFamily();
    }

    public LiveData<List<Member>> getAllMembers() {
        return allMembers;
    }

    public LiveData<Family> getFamily() {
        return family;
    }

    public void deleteMember(Member member) {
        memberRepository.delete(member);
    }

    public void updateFamily(Family family) {
        familyRepository.update(family);
    }

    public void createFamilyWithOwner(String familyName, String ownerName, String ownerColor,
                                      OnSetupCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                SessionManager session = SessionManager.getInstance();
                
                Family f = new Family(familyName);
                long familyId = db.familyDao().insert(f);
                
                Member owner = new Member(familyId, ownerName, true, ownerColor);
                // ذخیره userId برای سرپرست (کاربر لاگین شده)
                owner.setUserId(session.getUserId());
                long memberId = db.memberDao().insert(owner);
                
                // ذخیره localMemberId در session
                session.saveLocalMemberId(memberId);
                
                if (listener != null) listener.onComplete();
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    public interface OnSetupCompleteListener {
        void onComplete();
        void onError(String error);
    }
}
