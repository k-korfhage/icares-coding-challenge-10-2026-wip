Ext.define('IcaresCodingChallenge.view.appointments.AppointmentListViewController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.appointmentlistviewcontroller',

    init: function () {
        this.lookup('providerServiceSelection').on(
            'selectionchange',
            this.onProviderServiceSelectionChange,
            this
        );
    },

    onProviderServiceSelectionChange: function (selection, providerId, serviceId) {
        var store = this.lookup('appointmentsGrid').getStore();

        store.removeAll();

        if (!providerId || !serviceId) {
            return;
        }

        store.getProxy().setUrl('/service-providers/' + providerId + '/appointments');
        store.load({
            params: {
                serviceId: serviceId
            },
            callback: function (records, operation, successful) {
                if (!successful) {
                    Ext.toast('Die Termine konnten nicht geladen werden. Prüfen Sie, ob das Backend erreichbar ist.');
                }
            }
        });
    }
});
