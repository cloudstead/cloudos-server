App.ValetKeysController = Ember.ObjectController.extend({
	isAddServiceKeysOpened: false,

	isShhAllowed: false,

	selectedKeyType: App.KEY_TYPES.vendor,

	refreshKeyList: function() {
		this.set('model', Api.get_service_keys());
	},

	isFormShown: function() {
		return this.get("isAddServiceKeysOpened") ? "row" : "row hide";
	}.property("isAddServiceKeysOpened"),

	serviceKeyTypes: function() {
		return this.get("isShhAllowed") ?
			[App.KEY_TYPES.vendor, App.KEY_TYPES.customer] : [App.KEY_TYPES.vendor];
	}.property(),

	hasServiceKeys: function(){
		var hasKeys = !Ember.isNone(this.get('content')) && !Ember.isEmpty(this.get("content"));
		this.set("isAddServiceKeysOpened", !hasKeys);
		return hasKeys;
	}.property('content'),

	actions: {
		toggleAddServiceKeys: function() {
			this.set("isAddServiceKeysOpened", !this.get("isAddServiceKeysOpened"));
		},

		doNewServiceKey: function(){
			var error_msg = locate(Em.I18n.translations, 'errors');
			var pattern = /^[a-zA-Z0-9\-_]{0,40}$/i;


			if (!pattern.test(this.get("serviceKeyName"))) {
				this.set('requestMessages',
					App.RequestMessagesObject.create({
						json: {
							"status": 'error',
							"api_token" : null,
							"errors": {
								"serviceKeyName": error_msg.only_alphanumeric_no_space
							}
						}
					})
				);
			}
			else {
				if (this.get("selectedKeyType") === App.KEY_TYPES.vendor) {
					var response = Api.request_vendor_key(this.get("serviceKeyName"));
					if (response.statusCode === 500){
						alert("Key with this name already exists");
					}

				}
				else if (this.get("selectedKeyType") === App.KEY_TYPES.customer){
					Api.request_customer_key(this.get("serviceKeyName"));
				}

				this.refreshKeyList();
			}
		},

		doDeleteKey: function(serviceKey) {
			if (confirm(Em.I18n.translations.sections.valet_keys.delete_confirm)){
				Api.delete_service_key(serviceKey);
				this.refreshKeyList();
			}
		},

		doUnlockCloudOs: function() {
			this.transitionToRoute("unlock");
		}
	}
});
