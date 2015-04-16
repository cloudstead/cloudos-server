App.AliasesRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return Api.find_email_aliases();
	}
});
