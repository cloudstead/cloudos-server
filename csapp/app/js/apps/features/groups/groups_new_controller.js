App.GroupsNewController = Ember.Controller.extend({
	actions: {
		doAddGroup: function () {
			var new_group = App.Group.create(this._formData());

			// validate data
			var groupErrors = Validator.validateGroup(new_group);

			if (groupErrors.is_not_empty){
				this._handleGroupValidationError(groupErrors);
			}
			else{
				new_group.save() ?
					this.send('doReturnToGroups') :
					this._handleGroupSaveFailed(new_group);
			}
		},

		doCancel: function () {
			this.send('doReturnToGroups');
		},

		doReturnToGroups: function() {
			this.transitionToRoute('groups');
		}
	},

	_formData: function() {
		return {
			name: this.get('name'),
			recipients: App.GroupMembers.toArrayFromString(this.get('recipients')),
			info: {
				storageQuota: this.get('storageQuota'),
				description: this.get('description')
			}
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

	_handleGroupSaveFailed: function(group) {
		alert('error adding group: ' + group.name);
	}
});
