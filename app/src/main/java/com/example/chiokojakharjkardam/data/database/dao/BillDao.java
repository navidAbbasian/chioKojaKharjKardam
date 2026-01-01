package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Bill;

import java.util.List;

@Dao
public interface BillDao {

    @Insert
    long insert(Bill bill);

    @Update
    void update(Bill bill);

    @Delete
    void delete(Bill bill);

    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    LiveData<List<Bill>> getAllBills();

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    LiveData<List<Bill>> getUnpaidBills();

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    List<Bill> getUnpaidBillsSync();

    @Query("SELECT * FROM bills WHERE isPaid = 1 ORDER BY dueDate DESC")
    LiveData<List<Bill>> getPaidBills();

    @Query("SELECT * FROM bills WHERE id = :id")
    LiveData<Bill> getBillById(long id);

    @Query("SELECT * FROM bills WHERE id = :id")
    Bill getBillByIdSync(long id);

    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate <= :date ORDER BY dueDate ASC")
    List<Bill> getOverdueBills(long date);

    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    List<Bill> getBillsDueBetween(long startDate, long endDate);

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :id")
    void updatePaidStatus(long id, boolean isPaid);

    @Query("SELECT COUNT(*) FROM bills WHERE isPaid = 0")
    LiveData<Integer> getUnpaidBillCount();
}

