App.ResetPasswordRoute = CloudOSProtectedRoute.extend({
	model: function (params) {
		return { token : params['token'] };
	}
});
