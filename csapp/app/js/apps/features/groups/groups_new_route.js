App.GroupsNewRoute = Ember.Route.extend({
	model: function() {
		return this.modelFor('groups');
	}
});
