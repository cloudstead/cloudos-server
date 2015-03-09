App.InstalledappsController = Ember.ArrayController.extend({
	actions: {
		doUninstallApp: function(app) {
			if (confirm("You are about to uninstall " + app.name + ". Are sure You want to proceed?")){
				var task_id = Api.uninstall_cloud_app(app).uuid;
				if (!Ember.isNone(task_id)){
					this.watchTaskStatus(task_id, app);
				}
			}
		},

		sortBy: function(property){
			this.set('sortProperties',[property]);
			this.set('sortAscending', !this.get('sortAscending'));
		},
	},

	uninstallStatus: "",
	app_to_uninstall: null,
	isUninstallModalCloseHidden: true,

	watchTaskStatus: function(task_id, app){
		var self = this;

		self.set("app_to_uninstall", app);

		self.send('openModal','app_uninstall_modal');

		var statusInterval = setInterval(function(){
			var status = Api.get_task_results(task_id);

			console.log("status: ", status);

			if (!Ember.isEmpty(status.events)) {
				var message_key = status.events[status.events.length-1].messageKey;
				self.set('uninstallStatus', Em.I18n.translations.task[message_key]);
			}

			if (status.success) {
				self.set('uninstallStatus', Em.I18n.translations.task['{appUninstall.success}']);
				self.set('isUninstallModalCloseHidden', false);
				window.clearInterval(statusInterval);
			}

				result = Api.cloud_os_launch_status(self.get('model')["cloudOsRequest"]["name"]);
				if (result.history){
					last_status = result.history[result.history.length-1];
					$('#progressMeter').css('width', (result.history.length * 10) + '%');
					self.set('statusMessage', swapStatusMessage(last_status["messageKey"]));

					if (result.history.length >= 10){
						self.set('isInProgress', false);
						window.clearInterval(statusInterval);
					}
				}
			}, 5000);

		var status = Api.get_task_results(task_id);
	}
});
