App.TaskProgressModalComponent = Ember.Component.extend({

	isCloseButtonHidden: true,

	didInsertElement: function() {
		this.send("watchTaskStatus");
	},

	CloseButtonClass: function() {
		var cls = "large-12 columns";
		return cls + (this.get("isCloseButtonHidden") ? " hide" : "");
	}.property("isCloseButtonHidden"),

	actions: {
		closeModal: function() {
			console.log("action: ", this.get("closeAction"));

			this.sendAction("closeAction");
		},

		watchTaskStatus: function() {
			var self = this;
			var task_id = this.get("taskId");

			self.set('message', Em.I18n.translations.task.progress_modal.task_starting);
			self.set('isCloseButtonHidden', true);
			self.send("openModal", "app_install_modal");

			var statusInterval = setInterval(function(){
				var status = Api.get_task_results(task_id);

				if (status.success) {
					self.set('message', Em.I18n.translations.task.progress_modal.task_success);
					self.send("stopWatchingTaskStatus", statusInterval, true);
				} else if (!Ember.isEmpty(status.events)) {
					var message_key = status.events[status.events.length-1].messageKey;
					self.set('message', Em.I18n.translations.task.events[message_key]);
					if (status.error !== undefined) {
						self.send("stopWatchingTaskStatus", statusInterval, false);
					}
				}
			}, 5000);
		},

		stopWatchingTaskStatus: function(interval, taskSuccess) {
			this.set('isCloseButtonHidden', false);
			window.clearInterval(interval);
		}
	}
});
