package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class EventAdminAdminProjectService implements SettingServiceSupplier<AdminProject> {
	
	private AdminProject adminProject;
	
	public EventAdminAdminProjectService() {
		loadData();
	}
	
	private void loadData() {
		adminProject = new AdminProject(
			PROJECT_ID_VALUE,
			"이벤트 어드민",
			null,
			null
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