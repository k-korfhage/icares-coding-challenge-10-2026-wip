Ext.define('IcaresCodingChallenge.view.appointments.AppointmentListView', {
    extend: 'Ext.Container',
    xtype: 'appointmentlistview',
    cls: 'appointmentlistview',
    controller: {
        type: 'appointmentlistviewcontroller'
    },
    scrollable: true,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [
        {
            xtype: 'component',
            html: '<h1>Termine einsehen</h1>'
        },
        {
            xtype: 'container',
            cls: 'appointmentpanel',
            flex: 1,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [
                {
                    xtype: 'providerserviceselection',
                    reference: 'providerServiceSelection'
                },
                {
                    xtype: 'grid',
                    reference: 'appointmentsGrid',
                    flex: 1,
                    margin: '16 0 0 0',
                    emptyText: 'Keine Termine für die Auswahl gefunden.',
                    store: {
                        type: 'appointments'
                    },
                    columns: [
                        {
                            text: 'Kundennummer',
                            dataIndex: 'customerNumber',
                            flex: 1
                        },
                        {
                            text: 'Dienstleistung',
                            dataIndex: 'serviceName',
                            flex: 1
                        },
                        {
                            text: 'Beginn',
                            dataIndex: 'start',
                            flex: 1,
                            renderer: function (value) {
                                return Ext.Date.format(value, 'd.m.Y H:i');
                            }
                        },
                        {
                            text: 'Ende',
                            dataIndex: 'end',
                            flex: 1,
                            renderer: function (value) {
                                return Ext.Date.format(value, 'd.m.Y H:i');
                            }
                        }
                    ]
                }
            ]
        }
    ]
});
