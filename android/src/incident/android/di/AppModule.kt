package incident.android.di

import incident.android.data.api.AuthApi
import incident.android.data.api.IncidentApi
import incident.android.data.api.UserApi
import incident.android.data.repository.AuthRepository
import incident.android.data.repository.IncidentRepository
import incident.android.data.repository.UserRepository
import incident.android.data.store.IncidentDataStore
import incident.android.data.store.TokenPreferences
import incident.android.viewmodel.IncidentDetailViewModel
import incident.android.viewmodel.IncidentManagementViewModel
import incident.android.viewmodel.LoginViewModel
import incident.android.viewmodel.MyIncidentDetailViewModel
import incident.android.viewmodel.MyIncidentListViewModel
import incident.android.viewmodel.RegisterViewModel
import incident.android.viewmodel.ReportIncidentViewModel
import incident.android.viewmodel.UserManagementViewModel
import incident.android.viewmodel.UserViewModel
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}

val dataModule = module {
    single { TokenPreferences(androidContext()) }
    single { IncidentDataStore(androidContext()) }
    single { AuthApi(get()) }
    single { UserApi(get(), get()) }
    single { IncidentApi(get(), get()) }
    single { AuthRepository(get(), get()) }
    single { UserRepository(get()) }
    single { IncidentRepository(get()) }
}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { MyIncidentListViewModel (get(), get(), get(), get(), get()) }
    viewModel { MyIncidentDetailViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { ReportIncidentViewModel(get(), get()) }
    viewModel { UserViewModel(get()) }
    viewModel { UserManagementViewModel(get(), get())}
    viewModel { IncidentManagementViewModel(get(), get())}
    viewModel { IncidentDetailViewModel(get(), get(), get()) }
}

