App.GroupsRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return App.Group.findAll();
	}
});
