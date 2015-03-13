App.KeysController = Ember.ObjectController.extend({
	needs: "index",

	restoreKey: Ember.computed.alias("controllers.index.setup_response.restoreKey"),

	actions: {
		doComplete: function(){
			window.location.replace('/admin.html');
		}
	},
});
