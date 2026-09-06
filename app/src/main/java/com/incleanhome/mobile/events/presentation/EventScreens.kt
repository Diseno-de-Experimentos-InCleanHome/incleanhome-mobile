package com.incleanhome.mobile.events.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.events.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EventsScreen(viewModel:EventsViewModel,clientView:Boolean,onBack:()->Unit,onCreate:()->Unit,onEvent:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState(); Column(modifier.fillMaxSize().padding(16.dp)){
        Header(if(clientView)"Mis eventos" else "Eventos disponibles",onBack)
        if(clientView){Spacer(Modifier.height(8.dp));Button(onClick=onCreate,modifier=Modifier.fillMaxWidth()){Text("Crear evento")}}
        Spacer(Modifier.height(8.dp));Button(onClick=viewModel::refresh,modifier=Modifier.fillMaxWidth()){Text("Actualizar")};Spacer(Modifier.height(12.dp))
        LoadableList(s.loading,s.error,s.events.isEmpty(),if(clientView)"Aún no has creado eventos." else "No hay eventos abiertos.",viewModel::refresh){
            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(s.events,key=Event::id){e->EventCard(e){onEvent(e.id)}}}
        }
    }
}

@Composable
fun CreateEventScreen(viewModel:CreateEventViewModel,onBack:()->Unit,onDone:()->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Header("Crear evento",onBack)
        if(s.created!=null){Text("Evento creado correctamente",style=MaterialTheme.typography.headlineSmall);Text(s.created?.title.orEmpty());Button(onClick=onDone,modifier=Modifier.fillMaxWidth()){Text("Volver a Mis eventos")};return@Column}
        Field(s.title,viewModel::title,"Título",s.submitting);Field(s.description,viewModel::description,"Descripción (opcional)",s.submitting,3)
        Field(s.services,viewModel::services,"Tipos de servicio separados por coma",s.submitting)
        Field(s.zone,viewModel::zone,"Zona",s.submitting);Field(s.address,viewModel::address,"Dirección",s.submitting)
        Field(s.date,viewModel::date,"Fecha (yyyy-MM-dd)",s.submitting);Field(s.start,viewModel::start,"Hora de inicio (HH:mm)",s.submitting)
        Field(s.end,viewModel::end,"Hora de fin (HH:mm)",s.submitting);Field(s.workers,viewModel::workers,"Cantidad de workers",s.submitting,keyboard=KeyboardType.Number)
        Field(s.rate,viewModel::rate,"Tarifa por hora ofrecida",s.submitting,keyboard=KeyboardType.Decimal)
        Field(s.deadline,viewModel::deadline,"Deadline ISO-8601 UTC",s.submitting)
        s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick=viewModel::submit,modifier=Modifier.fillMaxWidth(),enabled=!s.submitting){if(s.submitting)CircularProgressIndicator(strokeWidth=2.dp)else Text("Publicar evento")}
    }
}

@Composable
fun EventDetailScreen(viewModel:EventDetailViewModel,workerView:Boolean,onBack:()->Unit,onApplications:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Header("Detalle del evento",onBack)
        Button(onClick=viewModel::refresh,modifier=Modifier.fillMaxWidth()){Text("Actualizar")}
        when{ s.loading->CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally));s.event==null->{Text(s.error.orEmpty(),color=MaterialTheme.colorScheme.error);Button(onClick=viewModel::refresh){Text("Reintentar")}}
            else->s.event?.let{e->
                EventDetails(e)
                if(workerView){
                    if(e.myApplicationStatus!=null) Text("Estado de tu postulación: ${e.myApplicationStatus}",color=MaterialTheme.colorScheme.primary)
                    else if(canApply(e)){
                        Field(s.message,viewModel::message,"Mensaje de postulación (opcional)",s.applying,3)
                        Button(onClick=viewModel::apply,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text("Postular")}
                    } else Text("Este evento no admite nuevas postulaciones.")
                }else{
                    Button(onClick={onApplications(e.id)},modifier=Modifier.fillMaxWidth()){Text("Ver postulaciones")}
                    if(e.status==EventStatus.OPEN||e.status==EventStatus.STAFFED)Button(onClick=viewModel::cancel,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text("Cancelar evento")}
                    val eventDate = runCatching { LocalDate.parse(e.date) }.getOrNull()
                    if(e.status==EventStatus.STAFFED && eventDate != null && !eventDate.isAfter(LocalDate.now(ZoneOffset.UTC)))Button(onClick=viewModel::complete,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text("Marcar completado")}
                }
                if(s.applying)CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally));s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};s.success?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
            }
        }
    }
}

@Composable
fun ApplicationsScreen(viewModel:ApplicationsViewModel,onBack:()->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().padding(16.dp)){Header("Postulaciones",onBack);Spacer(Modifier.height(8.dp));Button(onClick=viewModel::refresh,modifier=Modifier.fillMaxWidth()){Text("Actualizar")};Spacer(Modifier.height(12.dp))
        LoadableList(s.loading,s.error,s.applications.isEmpty(),"No hay postulaciones.",viewModel::refresh){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(s.applications,key=EventApplication::id){a->ApplicationCard(a){
            if(a.status==ApplicationStatus.PENDING)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={viewModel.accept(a.id)},enabled=s.updating==null,modifier=Modifier.weight(1f)){Text("Aceptar")};Button(onClick={viewModel.reject(a.id)},enabled=s.updating==null,modifier=Modifier.weight(1f)){Text("Rechazar")}}
        }}}}
    }
}

@Composable
fun MyApplicationsScreen(viewModel:MyApplicationsViewModel,onBack:()->Unit,onEvent:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().padding(16.dp)){Header("Mis postulaciones",onBack);Spacer(Modifier.height(8.dp));Button(onClick=viewModel::refresh,modifier=Modifier.fillMaxWidth()){Text("Actualizar")};Spacer(Modifier.height(12.dp))
        LoadableList(s.loading,s.error,s.applications.isEmpty(),"Aún no tienes postulaciones.",viewModel::refresh){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(s.applications,key=EventApplication::id){a->ApplicationCard(a,onClick={onEvent(a.eventId)}){
            if(a.status==ApplicationStatus.PENDING)Button(onClick={viewModel.withdraw(a)},enabled=s.updating==null,modifier=Modifier.fillMaxWidth()){Text("Retirar postulación")}
        }}}}
    }
}

@Composable private fun EventCard(e:Event,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=onClick)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(e.title,style=MaterialTheme.typography.titleLarge);Text("${e.date}, ${e.startTime} - ${e.endTime}");Text("Zona: ${e.zone}");Text("Workers: ${e.acceptedCount}/${e.workersNeeded}");Text("Estado: ${e.status}")}}}
@Composable private fun EventDetails(e:Event){Text(e.title,style=MaterialTheme.typography.headlineSmall);Text("Publicado por: ${e.clientName}");if(e.description.isNotBlank())Text(e.description);Text("Servicios: ${e.serviceTypes.joinToString()}");Text("Zona: ${e.zone}");Text("Dirección: ${e.address}");Text("Fecha: ${e.date}, ${e.startTime} - ${e.endTime}");Text("Duración: ${e.hours.toPlainString()} horas");Text("Workers aceptados: ${e.acceptedCount}/${e.workersNeeded}");Text("Tarifa por hora: ${e.hourlyRateOffered.toPlainString()}");Text("Deadline: ${e.applicationDeadline}");Text("Estado: ${e.status}")}
@Composable private fun ApplicationCard(a:EventApplication,onClick:(()->Unit)?=null,content: @Composable ()->Unit){val m=Modifier.fillMaxWidth().let{if(onClick==null)it else it.clickable(onClick=onClick)};Card(m){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(a.workerName,style=MaterialTheme.typography.titleMedium);Text(a.eventTitle);Text("${a.eventDate} · ${a.eventZone}");a.message?.takeIf(String::isNotBlank)?.let{Text("Mensaje: $it")};Text("Estado: ${a.status}");content()}}}
@Composable private fun LoadableList(loading:Boolean,error:String?,empty:Boolean,emptyText:String,retry:()->Unit,content: @Composable ()->Unit){when{loading->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){CircularProgressIndicator()};error!=null->Column(horizontalAlignment=Alignment.CenterHorizontally){Text(error,color=MaterialTheme.colorScheme.error);Button(onClick=retry){Text("Reintentar")}};empty->Text(emptyText);else->content()}}
@Composable private fun Header(title:String,onBack:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Button(onClick=onBack){Text("Volver")};Text(title,Modifier.padding(start=16.dp),style=MaterialTheme.typography.headlineSmall)}}
@Composable private fun Field(value:String,onChange:(String)->Unit,label:String,disabled:Boolean,minLines:Int=1,keyboard:KeyboardType=KeyboardType.Text){OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},enabled=!disabled,minLines=minLines,maxLines=if(minLines>1)6 else 1,keyboardOptions=KeyboardOptions(keyboardType=keyboard))}
private fun canApply(e:Event):Boolean=e.status==EventStatus.OPEN&&e.myApplicationStatus==null&&runCatching{Instant.now()<Instant.parse(e.applicationDeadline)}.getOrDefault(false)
