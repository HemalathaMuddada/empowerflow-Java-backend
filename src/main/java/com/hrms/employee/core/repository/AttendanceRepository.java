package com.hrms.employee.core.repository;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndWorkDate(User employee, LocalDate workDate);
    List<Attendance> findByEmployeeAndWorkDateBetweenOrderByWorkDateAsc(User employee, LocalDate startDate, LocalDate endDate);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.workDate = :workDate " +
           "AND a.totalHours IS NOT NULL AND a.totalHours < :minHours " +
           "AND (a.isRegularized = false OR a.isRegularized IS NULL) " +
           "AND a.underworkAlertSentAt IS NULL")
    List<Attendance> findUnderworkedAttendanceForAlert(
            @Param("workDate") LocalDate workDate,
            @Param("minHours") double minHours
    );

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.workDate = :workDate " +
           "AND a.loginTime IS NOT NULL AND a.logoutTime IS NULL " +
           "AND a.missedLogoutAlertSentAt IS NULL")
    List<Attendance> findAttendanceWithMissedLogoutForAlert(@Param("workDate") LocalDate workDate);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.workDate = :workDate " +
           "AND a.loginTime IS NOT NULL AND a.logoutTime IS NOT NULL " +
           "AND a.totalHours IS NOT NULL AND a.totalHours < :minHours " +
           "AND (a.isRegularized = false OR a.isRegularized IS NULL) " +
           "AND a.earlyLogoutAlertSentAt IS NULL")
    List<Attendance> findEarlyLogoutsForAlert(@Param("workDate") LocalDate workDate, @Param("minHours") double minHours);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.workDate = :workDate " +
           "AND a.loginTime IS NOT NULL AND FUNCTION('TIME', a.loginTime) > :lateThreshold " +
           "AND (a.isRegularized = false OR a.isRegularized IS NULL) " +
           "AND a.lateLoginAlertSentAt IS NULL")
    List<Attendance> findLateLoginsForAlert(@Param("workDate") LocalDate workDate, @Param("lateThreshold") LocalTime lateThreshold);

    boolean existsByEmployeeAndWorkDate(User employee, LocalDate workDate);
}
