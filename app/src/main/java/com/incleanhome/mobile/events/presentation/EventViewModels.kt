package com.incleanhome.mobile.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.events.data.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(val loading: Boolean = true, val events: List<Event> = emptyList(), val error: String? = null)
class EventsViewModel(private val clientView: Boolean, private val repo: EventsRepository = EventsRepository()) : ViewModel() {
    private val _state = MutableStateFlow(EventsUiState()); val state: StateFlow<EventsUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() { _state.update { it.copy(loading = true, error = null) }; viewModelScope.launch {
        val result = if (clientView) repo.getMyEvents() else repo.searchOpen()
        when (result) {
            is EventResult.Success -> _state.value = EventsUiState(false, result.data)
            is EventResult.Error -> _state.value = EventsUiState(false, error = result.message)
        }
    } }
    companion object { fun Factory(clientView: Boolean) = factory { EventsViewModel(clientView) } }
}

data class CreateEventUiState(
    val title: String = "", val description: String = "", val services: String = "",
    val zone: String = "", val address: String = "",
    val date: String = LocalDate.now(ZoneOffset.UTC).plusDays(7).toString(),
    val start: String = "09:00", val end: String = "13:00", val workers: String = "1",
    val rate: String = "", val deadline: String = LocalDate.now(ZoneOffset.UTC).plusDays(6).toString() + "T18:00:00Z",
    val submitting: Boolean = false, val created: Event? = null, val error: String? = null
)
class CreateEventViewModel(private val repo: EventsRepository = EventsRepository()) : ViewModel() {
    private val _state = MutableStateFlow(CreateEventUiState()); val state: StateFlow<CreateEventUiState> = _state.asStateFlow()
    fun title(v:String)=edit{it.copy(title=v.take(120))}; fun description(v:String)=edit{it.copy(description=v.take(1000))}
    fun services(v:String)=edit{it.copy(services=v)}; fun zone(v:String)=edit{it.copy(zone=v.take(60))}
    fun address(v:String)=edit{it.copy(address=v.take(300))}; fun date(v:String)=edit{it.copy(date=v.take(10))}
    fun start(v:String)=edit{it.copy(start=v.take(5))}; fun end(v:String)=edit{it.copy(end=v.take(5))}
    fun workers(v:String)=edit{it.copy(workers=v.filter(Char::isDigit))}; fun rate(v:String)=edit{it.copy(rate=v)}
    fun deadline(v:String)=edit{it.copy(deadline=v)}
    fun submit() {
        val s=_state.value; if(s.submitting) return
        val eventDate=try{LocalDate.parse(s.date)}catch(_:DateTimeParseException){null}
        val start=try{LocalTime.parse(s.start)}catch(_:DateTimeParseException){null}
        val end=try{LocalTime.parse(s.end)}catch(_:DateTimeParseException){null}
        val deadline=try{Instant.parse(s.deadline)}catch(_:DateTimeParseException){null}
        val workers=s.workers.toIntOrNull(); val rate=s.rate.toBigDecimalOrNull()
        val serviceList=s.services.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        val error=when {
            s.title.isBlank() -> "El título es obligatorio."
            serviceList.isEmpty() -> "Ingresa al menos un tipo de servicio."
            s.zone.isBlank() -> "La zona es obligatoria."
            s.address.isBlank() -> "La dirección es obligatoria."
            eventDate==null || eventDate.isBefore(LocalDate.now(ZoneOffset.UTC)) -> "Usa una fecha válida que no esté en el pasado."
            start==null || end==null || !end.isAfter(start) -> "Usa horas HH:mm y una hora final posterior."
            workers==null || workers<1 -> "Se necesita al menos 1 worker."
            rate==null -> "Ingresa una tarifa por hora válida."
            deadline==null -> "Usa un deadline ISO-8601 UTC, por ejemplo 2026-09-10T18:00:00Z."
            deadline >= eventDate.atTime(start).toInstant(ZoneOffset.UTC) -> "El deadline debe ser anterior al inicio del evento."
            else -> null
        }
        if(error!=null){_state.update{it.copy(error=error)};return}
        val hours=BigDecimal.valueOf(Duration.between(start,end).toMinutes()).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP)
        val request=CreateEventRequest(s.title.trim(),s.description.trim().takeIf(String::isNotEmpty),serviceList,s.zone.trim(),s.address.trim(),s.date,s.start,s.end,hours,workers ?: return,rate ?: return,s.deadline)
        _state.update{it.copy(submitting=true,error=null)}; viewModelScope.launch { when(val r=repo.create(request)){
            is EventResult.Success->_state.update{it.copy(submitting=false,created=r.data)}
            is EventResult.Error->_state.update{it.copy(submitting=false,error=r.message)}
        } }
    }
    private fun edit(f:(CreateEventUiState)->CreateEventUiState){_state.update{f(it).copy(error=null)}}
    companion object { val Factory=factory{CreateEventViewModel()} }
}

data class EventDetailUiState(val loading:Boolean=true,val event:Event?=null,val applying:Boolean=false,val message:String="",val error:String?=null,val success:String?=null)
class EventDetailViewModel(private val id:Int, private val repo:EventsRepository=EventsRepository()):ViewModel(){
    private val _state=MutableStateFlow(EventDetailUiState()); val state:StateFlow<EventDetailUiState> = _state.asStateFlow(); init{refresh()}
    fun refresh(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.getEvent(id)){
        is EventResult.Success->_state.update{it.copy(loading=false,event=r.data)};is EventResult.Error->_state.update{it.copy(loading=false,error=r.message)}
    }}}
    fun message(v:String){_state.update{it.copy(message=v.take(500),error=null)}}
    fun apply(){if(_state.value.applying)return;_state.update{it.copy(applying=true,error=null)};viewModelScope.launch{when(val r=repo.apply(id,_state.value.message.trim().takeIf(String::isNotEmpty))){
        is EventResult.Success->{_state.update{it.copy(applying=false,message="",success="Postulación enviada.")};refresh()};is EventResult.Error->_state.update{it.copy(applying=false,error=r.message)}
    }}}
    fun cancel()=action(repoCall={repo.cancel(id)},"Evento cancelado.")
    fun complete()=action(repoCall={repo.complete(id)},"Evento completado.")
    private fun action(repoCall:suspend()->EventResult<Event>, success:String){if(_state.value.applying)return;_state.update{it.copy(applying=true,error=null)};viewModelScope.launch{when(val r=repoCall()){
        is EventResult.Success->_state.update{it.copy(applying=false,event=r.data,success=success)};is EventResult.Error->_state.update{it.copy(applying=false,error=r.message)}
    }}}
    companion object{fun Factory(id:Int)=factory{EventDetailViewModel(id)}}
}

data class ApplicationsUiState(val loading:Boolean=true,val applications:List<EventApplication> = emptyList(),val updating:Int?=null,val error:String?=null)
class ApplicationsViewModel(private val eventId:Int,private val repo:EventsRepository=EventsRepository()):ViewModel(){
    private val _state=MutableStateFlow(ApplicationsUiState());val state:StateFlow<ApplicationsUiState> = _state.asStateFlow();init{refresh()}
    fun refresh(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.getApplications(eventId)){
        is EventResult.Success->_state.value=ApplicationsUiState(false,r.data);is EventResult.Error->_state.value=ApplicationsUiState(false,error=r.message)
    }}}
    fun accept(id:Int)=update(id){repo.accept(eventId,id)};fun reject(id:Int)=update(id){repo.reject(eventId,id)}
    private fun update(id:Int,call:suspend()->EventResult<EventApplication>){_state.update{it.copy(updating=id,error=null)};viewModelScope.launch{when(val r=call()){
        is EventResult.Success->refresh();is EventResult.Error->_state.update{it.copy(updating=null,error=r.message)}
    }}}
    companion object{fun Factory(id:Int)=factory{ApplicationsViewModel(id)}}
}

data class MyApplicationsUiState(val loading:Boolean=true,val applications:List<EventApplication> = emptyList(),val updating:Int?=null,val error:String?=null)
class MyApplicationsViewModel(private val repo:EventsRepository=EventsRepository()):ViewModel(){
    private val _state=MutableStateFlow(MyApplicationsUiState());val state:StateFlow<MyApplicationsUiState> = _state.asStateFlow();init{refresh()}
    fun refresh(){_state.update{it.copy(loading=true,error=null)};viewModelScope.launch{when(val r=repo.getMyApplications()){
        is EventResult.Success->_state.value=MyApplicationsUiState(false,r.data);is EventResult.Error->_state.value=MyApplicationsUiState(false,error=r.message)
    }}}
    fun withdraw(a:EventApplication){_state.update{it.copy(updating=a.id,error=null)};viewModelScope.launch{when(val r=repo.withdraw(a.eventId,a.id)){
        is EventResult.Success->refresh();is EventResult.Error->_state.update{it.copy(updating=null,error=r.message)}
    }}}
    companion object{val Factory=factory{MyApplicationsViewModel()}}
}

private fun <T:ViewModel> factory(create:()->T):ViewModelProvider.Factory=object:ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST") override fun <V:ViewModel> create(modelClass:Class<V>):V=create() as V
}
