Ext.define('IcaresCodingChallenge.view.shared.ProviderServiceSelectionModel', {
	extend: 'Ext.app.ViewModel',
	alias: 'viewmodel.providerselectionviewmodel',
	data: {
		name: 'providerServiceSelection'
	},
	stores: {
		providers: {
			type: 'providers'
		},
		services: {
			type: 'services'
		}
	}
});
