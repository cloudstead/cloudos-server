App.GroupController = Ember.ObjectController.extend({
	actions: {
		doEditGroup: function () {
			var group = this.get('model');

			// validate data
			var groupErrors = Validator.validateGroup(group);

			if (groupErrors.is_not_empty){
				this._handleGroupValidationError(groupErrors);
			}
			else{
				group.updateWith(this._formData()) ?
					this.transitionToRoute('groups') :
					this._handleGroupUpdateFailed(group);
			}
		},

		doCancel: function () {
			this.transitionToRoute('groups');
		}
	},

	_formData: function() {
		return {
			name: this.get('name'),
			recipients: App.GroupMembers.toArrayFromString(this.get('recipients'))
		};
	},

	_handleGroupValidationError: function(error) {
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": error
				}
			})
		);
	},

	_handleGroupUpdateFailed: function(group) {
		alert('error updating group: ' + group.name)
	}
});
