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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.events.data.*
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.components.RefreshButton
import com.incleanhome.mobile.ui.components.ServiceTypeSelector
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatDateRange
import com.incleanhome.mobile.ui.format.formatDateTime
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.format.presentationValues
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EventsScreen(viewModel:EventsViewModel,clientView:Boolean,onBack:()->Unit,onCreate:()->Unit,onEvent:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState()
    ScreenBackground(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(stringResource(if(clientView) R.string.title_my_events else R.string.title_available_events), onBack, trailingContent = { RefreshButton(viewModel::refresh, showLabel = false) })
            if (clientView) {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(stringResource(R.string.title_create_event), onCreate)
            }
            Spacer(Modifier.height(12.dp))
            LoadableList(s.loading, s.error, s.events.isEmpty(), stringResource(if(clientView) R.string.empty_my_events else R.string.empty_available_events), viewModel::refresh) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.events, key = Event::id) { event -> EventCard(event) { onEvent(event.id) } }
                }
            }
        }
    }
}

@Composable
fun CreateEventScreen(viewModel:CreateEventViewModel,onBack:()->Unit,onDone:()->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        ScreenHeader(stringResource(R.string.title_create_event),onBack)
        if(s.created!=null){Text(stringResource(R.string.event_created),style=MaterialTheme.typography.headlineSmall);Text(s.created?.title.orEmpty());Button(onClick=onDone,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.title_my_events))};return@Column}
        Field(s.title,viewModel::title,stringResource(R.string.field_title),s.submitting);Field(s.description,viewModel::description,stringResource(R.string.field_description_optional),s.submitting,3)
        Text(stringResource(R.string.auth_services), style=MaterialTheme.typography.labelLarge)
        ServiceTypeSelector(
            selectedValues=parseServiceTypes(s.services),
            onSelectionChange={viewModel.services(it.joinToString(","))},
            enabled=!s.submitting
        )
        Field(s.zone,viewModel::zone,stringResource(R.string.field_zone),s.submitting);Field(s.address,viewModel::address,stringResource(R.string.field_address),s.submitting)
        Field(s.date,viewModel::date,stringResource(R.string.field_date_iso),s.submitting);Field(s.start,viewModel::start,stringResource(R.string.field_start_time),s.submitting)
        Field(s.end,viewModel::end,stringResource(R.string.field_end_time),s.submitting);Field(s.workers,viewModel::workers,stringResource(R.string.field_workers_count),s.submitting,keyboard=KeyboardType.Number)
        Field(s.rate,viewModel::rate,stringResource(R.string.field_hourly_rate_offered),s.submitting,keyboard=KeyboardType.Decimal)
        Field(s.deadline,viewModel::deadline,stringResource(R.string.field_deadline_iso),s.submitting)
        s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick=viewModel::submit,modifier=Modifier.fillMaxWidth(),enabled=!s.submitting){if(s.submitting)CircularProgressIndicator(strokeWidth=2.dp)else Text(stringResource(R.string.event_publish))}
    }
}

@Composable
fun EventDetailScreen(viewModel:EventDetailViewModel,workerView:Boolean,onBack:()->Unit,onApplications:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        ScreenHeader(stringResource(R.string.title_event_detail),onBack, trailingContent = { RefreshButton(viewModel::refresh, showLabel = false) })
        when{ s.loading->LoadingState();s.event==null->{ErrorRetryState(s.error.orEmpty(),viewModel::refresh)}
            else->s.event?.let{e->
                EventDetails(e)
                if(workerView){
                    if(e.myApplicationStatus!=null) Text(stringResource(R.string.event_application_status,presentationValue(e.myApplicationStatus)),color=MaterialTheme.colorScheme.primary)
                    else if(canApply(e)){
                        Field(s.message,viewModel::message,stringResource(R.string.field_application_message_optional),s.applying,3)
                        Button(onClick=viewModel::apply,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text(stringResource(R.string.event_apply))}
                    } else Text(stringResource(R.string.event_no_applications_allowed))
                }else{
                    Button(onClick={onApplications(e.id)},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.event_view_applications))}
                    if(e.status==EventStatus.OPEN||e.status==EventStatus.STAFFED)Button(onClick=viewModel::cancel,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text(stringResource(R.string.event_cancel))}
                    val eventDate = runCatching { LocalDate.parse(e.date) }.getOrNull()
                    if(e.status==EventStatus.STAFFED && eventDate != null && !eventDate.isAfter(LocalDate.now(ZoneOffset.UTC)))Button(onClick=viewModel::complete,modifier=Modifier.fillMaxWidth(),enabled=!s.applying){Text(stringResource(R.string.booking_mark_completed))}
                }
                if(s.applying)CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally));s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};s.success?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
            }
        }
    }
}

@Composable
fun ApplicationsScreen(viewModel:ApplicationsViewModel,onBack:()->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().padding(16.dp)){ScreenHeader(stringResource(R.string.title_applications),onBack, trailingContent = { RefreshButton(viewModel::refresh, showLabel = false) });Spacer(Modifier.height(12.dp))
        LoadableList(s.loading,s.error,s.applications.isEmpty(),stringResource(R.string.empty_applications),viewModel::refresh){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(s.applications,key=EventApplication::id){a->ApplicationCard(a){
            if(a.status==ApplicationStatus.PENDING)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={viewModel.accept(a.id)},enabled=s.updating==null,modifier=Modifier.weight(1f)){Text(stringResource(R.string.action_accept))};Button(onClick={viewModel.reject(a.id)},enabled=s.updating==null,modifier=Modifier.weight(1f)){Text(stringResource(R.string.action_reject))}}
        }}}}
    }
}

@Composable
fun MyApplicationsScreen(viewModel:MyApplicationsViewModel,onBack:()->Unit,onEvent:(Int)->Unit,modifier:Modifier=Modifier){
    val s by viewModel.state.collectAsState();Column(modifier.fillMaxSize().padding(16.dp)){ScreenHeader(stringResource(R.string.title_my_applications),onBack, trailingContent = { RefreshButton(viewModel::refresh, showLabel = false) });Spacer(Modifier.height(12.dp))
        LoadableList(s.loading,s.error,s.applications.isEmpty(),stringResource(R.string.empty_my_applications),viewModel::refresh){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(s.applications,key=EventApplication::id){a->ApplicationCard(a,onClick={onEvent(a.eventId)}){
            if(a.status==ApplicationStatus.PENDING)Button(onClick={viewModel.withdraw(a)},enabled=s.updating==null,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.event_withdraw_application))}
        }}}}
    }
}

@Composable private fun EventCard(e:Event,onClick:()->Unit){InCleanHomeCard(onClick=onClick){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(e.title,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.secondary);Text(stringResource(R.string.label_services,presentationValues(e.serviceTypes)));Text(formatDateRange(e.date,e.startTime,e.endTime));Text(stringResource(R.string.label_zone,e.zone));Text(stringResource(R.string.event_workers,e.acceptedCount,e.workersNeeded));Text(stringResource(R.string.label_status,presentationValue(e.status)),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}}}
@Composable private fun EventDetails(e:Event){Text(e.title,style=MaterialTheme.typography.headlineSmall);Text(stringResource(R.string.event_published_by,e.clientName));if(e.description.isNotBlank())Text(e.description);Text(stringResource(R.string.label_services,presentationValues(e.serviceTypes)));Text(stringResource(R.string.label_zone,e.zone));Text(stringResource(R.string.label_address,e.address));Text(stringResource(R.string.label_date,formatDateRange(e.date,e.startTime,e.endTime)));Text(stringResource(R.string.label_duration_hours,e.hours.toPlainString()));Text(stringResource(R.string.event_workers_accepted,e.acceptedCount,e.workersNeeded));Text(stringResource(R.string.label_hourly_rate_named,formatCurrency(e.hourlyRateOffered)));Text(stringResource(R.string.event_deadline,formatDateTime(e.applicationDeadline)));Text(stringResource(R.string.label_status,presentationValue(e.status)))}
@Composable private fun ApplicationCard(a:EventApplication,onClick:(()->Unit)?=null,content: @Composable ()->Unit){InCleanHomeCard(onClick=onClick){Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Text(a.workerName,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.secondary);Text(a.eventTitle);Text("${formatDate(a.eventDate)} · ${a.eventZone}");a.message?.takeIf(String::isNotBlank)?.let{Text(stringResource(R.string.event_message,it))};Text(stringResource(R.string.label_status,presentationValue(a.status)));content()}}}
@Composable private fun LoadableList(loading:Boolean,error:String?,empty:Boolean,emptyText:String,retry:()->Unit,content: @Composable ()->Unit){when{loading->LoadingState();error!=null->ErrorRetryState(error,retry);empty->EmptyState(emptyText);else->content()}}
@Composable private fun Field(value:String,onChange:(String)->Unit,label:String,disabled:Boolean,minLines:Int=1,keyboard:KeyboardType=KeyboardType.Text){InCleanHomeTextField(value,onChange,label,Modifier.fillMaxWidth(),enabled=!disabled,singleLine=minLines==1,minLines=minLines,maxLines=if(minLines>1)6 else 1,keyboardOptions=KeyboardOptions(keyboardType=keyboard))}
private fun canApply(e:Event):Boolean=e.status==EventStatus.OPEN&&e.myApplicationStatus==null&&runCatching{Instant.now()<Instant.parse(e.applicationDeadline)}.getOrDefault(false)
private fun parseServiceTypes(value: String): List<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
