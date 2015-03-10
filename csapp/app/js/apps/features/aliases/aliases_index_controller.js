App.AliasesIndexController = Ember.ObjectController.extend({
	actions: {
		doRemoveAlias: function (name) {
			if (!Api.remove_email_alias(name)) {
				alert('error removing alias: '+name);
			}
		}
	}
});
