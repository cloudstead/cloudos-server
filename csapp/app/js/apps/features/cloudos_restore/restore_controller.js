App.RestoreController = Ember.ObjectController.extend({
	setup_key: function() {
		try{
			return getParameterByName('key');
		}catch(err){
			//
		}
	}.property(),

	user_message_priority: "alert-box",

	user_message: "Initializing restore...",

	isHidden: true,

	actions: {
		doRestore: function(){
			var self = this;

			var restore_data = {
				setupKey: this.get('setup_key'),
				initialPassword: this.get('initialPassword'),
				restoreKey: this.get('restoreKey'),
				notifyEmail: this.get('notifyEmail')
			};

			var validate = this.validate(restore_data);

			if (this.hasValidationFailed(validate)){
				this.set('requestMessages',
							App.RequestMessagesObject.create({
								json: {
									status: 'error',
									api_token : null,
									errors: validate
								}
							})
						);
			}
			else {
				var task_id = Api.restore(restore_data).uuid;
				self.send('openModal','user_message_modal');

				var statusInterval = setInterval(function(){
					result = Api.get_task_results(task_id);

					if (self.hasRestoreTaskFailed(result)){
						var last_error = result.events[result.events.length-1].messageKey
						self.set("user_message", Em.I18n.translations.task.events[last_error]);
						self.set("user_message_priority", "alert-box alert");
						self.set("isHidden", false);
					}
					else if (self.isShuttingDown(result)){
						self.set("user_message", Em.I18n.translations.task.events['{restore.shutting_down}']);
						self.set("user_message_priority", "alert-box");
					}
					else if (self.hasRestoreTaskSucceded(result)){
						window.clearInterval(statusInterval);
						self.set("user_message", Em.I18n.translations.task.events['{restore.done}']);
						self.set("user_message_priority", "alert-box success");
					}
					else{
						self.set("user_message", Em.I18n.translations.task.events[result.actionMessageKey]);
						self.set("user_message_priority", "alert-box");
					}
				}, 5000);
			}
		}
	},

	validate: function(data) {
		var error_msg = locate(Em.I18n.translations, 'errors');
		var response = {
			setupKey:null,
			initialPassword:null,
			notifyEmail:null,
			restoreKey:null,
		};

		if (Ember.isEmpty(data.setupKey)){
			response.setupKey = error_msg.setup_key_missing;
		}
		if (Ember.isEmpty(data.initialPassword)){
			response.initialPassword = error_msg.field_required;
		}else if(data.initialPassword.length < 8) {
			response.initialPassword = error_msg.password_short;
		}
		if (Ember.isEmpty(data.notifyEmail)){
			response.notifyEmail = error_msg.field_required;
		}
		if (Ember.isEmpty(data.restoreKey)){
			response.restoreKey = error_msg.field_required;
		}

		return response;
	},

	hasValidationFailed: function(validation) {
		return !Ember.isNone(validation.setupKey) || !Ember.isNone(validation.restoreKey) ||
			!Ember.isNone(validation.initialPassword) || !Ember.isNone(validation.notifyEmail);
	},

	hasRestoreTaskSucceded: function(task) {
		return task.status === 'error' && task.jqXHR.status == 404;
	},

	hasRestoreTaskFailed: function(task) {
		return !Ember.isEmpty(task.error);
	},

	isShuttingDown: function(task) {
		return task.status === 'error' && task.jqXHR.status == 503;
	}
});
