Ext.define('IcaresCodingChallenge.view.shared.ProviderServiceSelection',{
	xtype: 'providerserviceselection',
	controller: {type: 'providerselectioncontroller'},
	viewModel: {type: 'providerselectionviewmodel'},
	requires: ['Ext.field.ComboBox'],
	extend: 'Ext.Container',
  layout: {
    type: 'vbox',
    align: 'stretch'
  },
  items: [
    {
      xtype: 'component',
      margin: '0 0 16 0',
      html: '<p>Wählen Sie einen IT-Service-Anbieter und eine Dienstleistung aus.</p>'
    },
    {
      xtype: 'combobox',
      reference: 'providerCombo',
      label: 'Anbieter',
      placeholder: 'Anbieter werden geladen …',
      displayField: 'name',
      valueField: 'providerId',
      queryMode: 'local',
      clearable: true,
      bind: {
        store: '{providers}'
      },
      listeners: {
        change: 'onProviderChange',
        painted: 'onProviderComboPainted'
      }
    },
    {
      xtype: 'combobox',
      reference: 'serviceCombo',
      label: 'Dienstleistung',
      placeholder: 'Zuerst einen Anbieter auswählen',
      displayField: 'name',
      valueField: 'serviceId',
      itemTpl: '<strong>{code:htmlEncode}</strong> · {name:htmlEncode}',
      displayTpl: '{code} · {name}',
      queryMode: 'local',
      clearable: true,
      disabled: true,
      bind: {
        store: '{services}'
      },
      listeners: {
        change: 'onServiceChange'
      }
    }
  ]
});
