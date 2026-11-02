Ext.define('IcaresCodingChallenge.store.Appointments', {
    extend: 'Ext.data.Store',
    alias: 'store.appointments',

    fields: [
        { name: 'id', type: 'int' },
        { name: 'customerNumber', type: 'string' },
        { name: 'start', type: 'date' },
        { name: 'end', type: 'date' },
        { name: 'serviceName', type: 'string' }
    ],

    autoLoad: false,

    proxy: {
        type: 'ajax',
        url: '/service-providers',
        reader: {
            type: 'json',
            rootProperty: ''
        }
    }
});
