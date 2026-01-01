package com.example.chiokojakharjkardam.ui.members;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.repository.FamilyRepository;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;

public class AddMemberViewModel extends AndroidViewModel {

    private final MemberRepository memberRepository;
    private final FamilyRepository familyRepository;

    public AddMemberViewModel(@NonNull Application application) {
        super(application);
        memberRepository = new MemberRepository(application);
        familyRepository = new FamilyRepository(application);
    }

    public LiveData<Family> getFamily() {
        return familyRepository.getFirstFamily();
    }

    public LiveData<Member> getMemberById(long id) {
        return memberRepository.getMemberById(id);
    }

    public void insertMember(Member member) {
        memberRepository.insert(member);
    }

    public void updateMember(Member member) {
        memberRepository.update(member);
    }
}

