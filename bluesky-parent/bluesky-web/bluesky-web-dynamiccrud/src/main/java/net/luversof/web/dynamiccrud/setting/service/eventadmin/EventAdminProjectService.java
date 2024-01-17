package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.*;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class EventAdminProjectService implements SettingServiceSupplier<Project> {
	
	@Getter private Project project;
	
	public EventAdminProjectService() {
		loadData();
	}
	
	private void loadData() {
		project = new Project(
			KEY_ADMIN_PROJECT,
			KEY_PROJECT,
			"Event Admin"
		); 
	}

	@Override
	public Project findOne(SettingParameter settingParameter) {
		if (project.getAdminProjectId().equals(settingParameter.adminProjectId()) && project.getProjectId().equals(settingParameter.projectId())) {
			return project;
		}
		return null;
	}

}
