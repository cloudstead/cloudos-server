App.SettingsController = Ember.ObjectController.extend({
	actions: {
		changePassword: function () {
			var newPassword = this.get('new_password');
			var confirmPassword = this.get('new_password2');
			var error_msg = locate(Em.I18n.translations, 'errors');

			var validate = this.validateSettings(this.get('current_password'), this.get('new_password'), this.get('new_password2'));
			if ( (validate.current_password) || (validate.new_password) || (validate.confirm_new_password)){
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"current_password": validate.current_password,
									"new_password": validate.new_password,
									"new_password2": validate.confirm_new_password}}
					  })
					);
				return false;
			}

			var pass_change =
				Api.change_password(CloudOs.account().name, this.get('current_password'), newPassword);

			if (pass_change.status !== 'error') {
				this.send('showFlashMessage', Em.I18n.translations.notifications.password_changed_successfully);
				this.transitionTo('index');
			} else {
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"current_password": error_msg.password_incorrect}}
					  })
					);
			}
		}
	},
	validateSettings: function(curr_password, new_password, confirm_new_password){

		var response = {"current_password":null,
						"new_password": null,
						"confirm_new_password":null};

		var error_msg = locate(Em.I18n.translations, 'errors');

		if ((!curr_password) || (curr_password.trim() == '')){
			response.current_password = error_msg.field_required;
		}

		if ((!new_password) || (new_password.trim() == '')){
			response.new_password = error_msg.field_required;
		}else if(new_password.length < 8) {
			response.new_password = error_msg.password_short;
		}

		if ((!confirm_new_password) || (confirm_new_password.trim() == '')){
			response.confirm_new_password = error_msg.field_required;
		}

		if (new_password != confirm_new_password) {
			response.confirm_new_password = error_msg.password_mismatch;
		}

		return response;
	}
});
