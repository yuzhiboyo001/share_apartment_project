package com.example.apartment.repository;

import com.example.apartment.entity.Room;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("SELECT r FROM Room r WHERE r.currentUserId = ?1")
    Room findByCurrentUserId(Long userId);

    List<Room> findByApartmentId(Long apartmentId);

    Page<Room> findByApartmentId(Long apartmentId, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.apartmentId = :apartmentId AND r.status = :status")
    List<Room> findByApartmentIdAndStatus(@Param("apartmentId") Long apartmentId, @Param("status") Integer status);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.apartmentId = :apartmentId AND r.status = 1")
    long countOccupiedRooms(@Param("apartmentId") Long apartmentId);

    // 查询公寓的空置房间
    @Query("SELECT r FROM Room r WHERE r.apartmentId = :apartmentId AND r.status = 0")
    List<Room> findVacantRooms(@Param("apartmentId") Long apartmentId);


    // 检查房间名是否已存在
    boolean existsByApartmentIdAndRoomNumber(Long apartmentId, String roomNumber);
}
