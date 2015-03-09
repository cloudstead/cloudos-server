App.InstalledappsRoute = Ember.Route.extend({
	model: function() {
		var installed_apps_info = Api.find_installed_apps();
		var app_names = installed_apps_info.appNames;
		var app_info = installed_apps_info.appVersions;
		var apps = [];

		if (!Ember.isEmpty(app_names) && !Ember.isEmpty(app_info)) {
			app_names.forEach(function(app_name) {
				apps.push(app_info[app_name][0]);
			});
		}

		return apps;
	},
	actions:{
		openModal: function(modalName){
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			this.controllerFor('restore').set("isUninstallModalCloseHidden", true);
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		}
	}
});
