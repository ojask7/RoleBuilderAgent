param environment string
param location string

resource openAi 'Microsoft.CognitiveServices/accounts@2023-05-01' = {
  name: 'aoai-${environment}'
  location: location
  sku: {
    name: 'S0'
  }
  kind: 'OpenAI'
  properties: {
    customSubDomainName: 'aoai-${environment}'
  }
}
