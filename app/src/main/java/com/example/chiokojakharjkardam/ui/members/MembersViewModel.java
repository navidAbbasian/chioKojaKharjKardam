package com.example.chiokojakharjkardam.ui.members;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;

import java.util.List;

public class MembersViewModel extends AndroidViewModel {

    private final MemberRepository repository;
    private final LiveData<List<Member>> allMembers;

    public MembersViewModel(@NonNull Application application) {
        super(application);
        repository = new MemberRepository(application);
        allMembers = repository.getAllMembers();
    }

    public LiveData<List<Member>> getAllMembers() {
        return allMembers;
    }

    public void deleteMember(Member member) {
        repository.delete(member);
    }
}

