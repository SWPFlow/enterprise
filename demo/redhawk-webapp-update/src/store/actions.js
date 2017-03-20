export const addDomainConfig = ({ commit }, domainConfig) => commit('addDomainConfig', domainConfig)
export const deleteDomainConfig = ( { commit }, index) => commit('deleteDomainConfig', index)
export const editDomainConfig = ({ commit }, index) => commit('editDomainConfig', index)
export const editDomainConfigName = ({ commit }, name) => commit('editDomainConfigName', name)
export const editDomainConfigNameServer = ({ commit }, nameServer) => commit('editDomainConfigNameServer', nameServer)
export const editDomainConfigDomainName = ({ commit }, domainName) => commit('editDomainConfigDomainName', domainName)
export const updateDomainConfig = ({ commit }) => commit('updateDomainConfig')
export const viewDomainConfig = ({ commit }, index) => commit('viewDomainConfig', index)
