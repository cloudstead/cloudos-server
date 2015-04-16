App.GroupRoute = CloudOSProtectedRoute.extend({
	model: function (params) {
		var group_data = App.Group.findByName(params['group_name']);

		return App.Group.create(
			{
				name: group_data.get('name'),
				recipients: App.GroupMembers.toStringFromArray(group_data.get('recipients'))
			}
		);
	}
});
