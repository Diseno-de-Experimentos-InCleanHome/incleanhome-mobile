package com.incleanhome.mobile.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.profile.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val PROFILE_NAME_REGEX = Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$")
private val PROFILE_PHONE_REGEX = Regex("^9\\d{8}$")
private fun profileNameError(name:String):String? { val value=name.trim(); return if(value.length !in 2..120 || !PROFILE_NAME_REGEX.matches(value)) "El nombre debe tener entre 2 y 120 caracteres y usar solo letras, espacios, apóstrofos o guiones." else null }
private fun profilePhoneError(phone:String?):String? { val value=phone?.trim().orEmpty(); return if(value.isNotEmpty() && !PROFILE_PHONE_REGEX.matches(value)) "El teléfono debe tener 9 dígitos y comenzar con 9." else null }

data class ClientProfileState(val loading:Boolean=true,val saving:Boolean=false,val profile:ClientProfile?=null,val error:String?=null,val success:String?=null)
class ClientProfileViewModel(private val repo:ProfileRepository=ProfileRepository()):ViewModel(){
 private val _state=MutableStateFlow(ClientProfileState()); val state=_state.asStateFlow()
 init{load()}; fun load(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.client()){is ProfileResult.Success->_state.update{it.copy(loading=false,profile=r.data)};is ProfileResult.Error->_state.update{it.copy(loading=false,error=r.message)}}}}
 fun save(name:String,phone:String?){if(_state.value.saving)return;val error=profileNameError(name)?:profilePhoneError(phone);if(error!=null){_state.update{it.copy(error=error,success=null)};return};_state.update{it.copy(saving=true,error=null,success=null)};viewModelScope.launch{when(val r=repo.updateClient(UpdateClientProfileRequest(name.trim(),phone?.trim()?.takeIf{it.isNotBlank()}))){is ProfileResult.Success->when(val fresh=repo.client()){is ProfileResult.Success->_state.update{it.copy(saving=false,profile=fresh.data,success="Perfil actualizado.")};is ProfileResult.Error->_state.update{it.copy(saving=false,error=fresh.message)}};is ProfileResult.Error->_state.update{it.copy(saving=false,error=r.message)}}}}
 companion object{val Factory=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:ViewModel>create(c:Class<T>):T=ClientProfileViewModel() as T}}
}

data class WorkerEditState(val loading:Boolean=true,val saving:Boolean=false,val profile:com.incleanhome.mobile.worker.data.WorkerProfile?=null,val error:String?=null,val success:String?=null)
class WorkerProfileEditViewModel(private val repo:ProfileRepository=ProfileRepository()):ViewModel(){
 private val _state=MutableStateFlow(WorkerEditState()); val state=_state.asStateFlow()
 init{load()}; fun load(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.worker()){is ProfileResult.Success->_state.update{it.copy(loading=false,profile=r.data)};is ProfileResult.Error->_state.update{it.copy(loading=false,error=r.message)}}}}
 fun save(name:String,phone:String?,age:String,experience:String,rate:String,services:String,zones:String,bio:String?){if(_state.value.saving)return;val a=age.toIntOrNull();val e=experience.toIntOrNull();val hr=rate.toBigDecimalOrNull();val serviceList=services.split(',').map(String::trim).filter(String::isNotBlank);val zoneList=zones.split(',').map(String::trim).filter(String::isNotBlank);val error=profileNameError(name)?:profilePhoneError(phone)?:when{a==null||a !in 18..70->"La edad debe estar entre 18 y 70 años.";e==null||e !in 0..50->"La experiencia debe estar entre 0 y 50 años.";hr==null||hr<BigDecimal.TEN||hr>BigDecimal("500")->"La tarifa debe estar entre 10 y 500.";serviceList.isEmpty()->"Selecciona al menos un servicio.";zoneList.isEmpty()->"Ingresa al menos una zona.";(bio?.trim()?.length ?: 0)>1000->"La biografía no puede superar 1000 caracteres.";else->null};if(error!=null){_state.update{it.copy(error=error,success=null)};return};_state.update{it.copy(saving=true,error=null,success=null)};viewModelScope.launch{when(val r=repo.updateWorker(UpdateWorkerProfileRequest(name.trim(),phone?.trim()?.takeIf{it.isNotBlank()},a!!,e!!,hr!!,serviceList,zoneList,bio?.trim()?.takeIf{it.isNotBlank()}))){is ProfileResult.Success->when(val fresh=repo.worker()){is ProfileResult.Success->_state.update{it.copy(saving=false,profile=fresh.data,success="Perfil actualizado.")};is ProfileResult.Error->_state.update{it.copy(saving=false,error=fresh.message)}};is ProfileResult.Error->_state.update{it.copy(saving=false,error=r.message)}}}}
 companion object{fun Factory()=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:ViewModel>create(c:Class<T>):T=WorkerProfileEditViewModel() as T}}
}
data class WorkerStatsState(val loading:Boolean=true,val stats:WorkerStats?=null,val error:String?=null)
class WorkerStatsViewModel(private val repo:ProfileRepository=ProfileRepository()):ViewModel(){private val _state=MutableStateFlow(WorkerStatsState());val state=_state.asStateFlow();init{load()};fun load(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.stats()){is ProfileResult.Success->_state.update{it.copy(loading=false,stats=r.data)};is ProfileResult.Error->_state.update{it.copy(loading=false,error=r.message)}}}};companion object{val Factory=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:ViewModel>create(c:Class<T>):T=WorkerStatsViewModel() as T}}}
