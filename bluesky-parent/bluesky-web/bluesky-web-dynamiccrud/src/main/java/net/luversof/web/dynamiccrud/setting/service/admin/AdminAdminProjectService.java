package net.luversof.web.dynamiccrud.setting.service.admin;

import static net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant.ADMIN_PROJECT_ID_VALUE;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class AdminAdminProjectService implements SettingServiceSupplier<AdminProject> {
	
	private AdminProject adminProject;
	
	public AdminAdminProjectService() {
		loadData();
	}
	
	private void loadData() {
		adminProject = new AdminProject(
			ADMIN_PROJECT_ID_VALUE,
			"어드민",
			"",
			"""
			ROLE_ADMIN > ROLE_USER
			ROLE_USER > ROLE_ANONYMOUS
			"""
		);
		
	}

	@Override
	public AdminProject findOne(SettingParameter settingParameter) {
		if (adminProject.getAdminProjectId().equals(settingParameter.adminProjectId())) {
			return adminProject;
		}
		return null;
	}

}