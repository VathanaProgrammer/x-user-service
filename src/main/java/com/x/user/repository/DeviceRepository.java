package com.x.user.repository;

import com.x.user.model.Device;
import com.x.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByUser(User user);
}
