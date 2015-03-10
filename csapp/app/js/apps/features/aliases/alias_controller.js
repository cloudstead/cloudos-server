App.AliasController = Ember.ObjectController.extend({
	actions: {
		doEditAlias: function () {
			var name = this.get('aliasName');
			var recipients = [];
			var recipient_names = this.get('recipients').split(",");
			for (var i=0; i<recipient_names.length; i++) {
				recipients.push(recipient_names[i].trim());
			}
			if (!Api.edit_email_alias({ 'name': name, 'recipients': recipients })) {
				alert('error editing alias: '+name);
			}
		}
	}
});
