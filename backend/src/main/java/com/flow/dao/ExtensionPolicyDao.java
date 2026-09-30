package com.flow.dao;

import com.flow.dto.ExtensionPolicy;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExtensionPolicyDao {

	List<ExtensionPolicy> findAll();

	ExtensionPolicy findByExtension(String extension);

	List<String> findBlockedExtensions();

	int countCustomForUpdate();

	void insertCustom(@Param("extension") String extension);

	int updateFixedBlocked(@Param("extension") String extension, @Param("blocked") boolean blocked);

	int deleteCustom(Long id);
}
