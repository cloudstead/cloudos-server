App.GroupsRoute = Ember.Route.extend({
	model: function () {
		return App.Group.findAll();
	}
});
