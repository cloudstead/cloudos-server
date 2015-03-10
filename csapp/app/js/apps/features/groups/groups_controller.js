App.GroupsController = Ember.ArrayController.extend({
	sortedGroups: function(){
		return this.get('arrangedContent');
	}.property('arrangedContent.@each'),

	actions: {
		doDeleteGroup: function (group_name) {
			var group = App.Group.getByName(group_name);
			if (!group.destroy()){
				this._handleGroupDeleteFailed(group);
			}
		}
	},

	_handleGroupDeleteFailed: function(group) {
		alert('error deleting group: ' + group.name);
	}
});
