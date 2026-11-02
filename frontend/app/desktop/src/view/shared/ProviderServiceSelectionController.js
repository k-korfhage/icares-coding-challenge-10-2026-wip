Ext.define('IcaresCodingChallenge.view.shared.ProviderServiceSelectionController', {
	extend: 'Ext.app.ViewController',
	alias: 'controller.providerselectioncontroller',

	providersLoadStarted: false,

	onProviderChange: function (combo, providerId) {
		var services = this.getViewModel().getStore('services'),
			serviceCombo = this.lookup('serviceCombo');

		services.removeAll();
		serviceCombo.clearValue();
		this.getView().fireEvent('selectionchange', this.getView(), providerId, null);

		if (!providerId) {
			serviceCombo.setDisabled(true);
			serviceCombo.setPlaceholder('Zuerst einen Anbieter auswählen');
			return;
		}

		serviceCombo.setDisabled(false);
		serviceCombo.setPlaceholder('Dienstleistungen werden geladen …');
		services.getProxy().setUrl('/service-providers/' + providerId + '/services');
		services.load({
			callback: function (records, operation, successful) {
				if (successful && records.length === 0) {
					serviceCombo.setPlaceholder('Keine Dienstleistungen gefunden');
				} else if (!successful) {
					serviceCombo.setPlaceholder('Dienstleistungen konnten nicht geladen werden');
					Ext.toast(
						'Die Dienstleistungen konnten nicht geladen werden. Prüfen Sie, ob das Backend erreichbar ist.'
					);
				}
			}
		});
	},

	onServiceChange: function (combo, serviceId) {
		if (serviceId) {
			this.getView().fireEvent(
				'selectionchange',
				this.getView(),
				this.lookup('providerCombo').getValue(),
				serviceId
			);
		}
	},

	onProviderComboPainted: function (combo) {
		var store = combo.getStore();

		if (this.providersLoadStarted) {
			return;
		}

		this.providersLoadStarted = true;
		store.load({
			callback: function (records, operation, successful) {
				if (successful && records.length === 0) {
					combo.setPlaceholder('Keine Anbieter gefunden');
				} else if (!successful) {
					combo.setPlaceholder('Anbieter konnten nicht geladen werden');
					Ext.toast(
						'Die Anbieterliste konnte nicht geladen werden. Prüfen Sie, ob das Backend erreichbar ist.'
					);
				}
			}
		});
	}
});
