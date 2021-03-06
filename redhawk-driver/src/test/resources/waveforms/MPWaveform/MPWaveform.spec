# RPM package for MPWaveform
# This file is regularly AUTO-GENERATED by the IDE. DO NOT MODIFY.

# By default, the RPM will install to the standard REDHAWK SDR root location (/var/redhawk/sdr)
# You can override this at install time using --prefix /new/sdr/root when invoking rpm (preferred method, if you must)
%{!?_sdrroot: %global _sdrroot /var/redhawk/sdr}
%define _prefix %{_sdrroot}
Prefix: %{_prefix}

Name: MPWaveform
Summary: Waveform MPWaveform
Version: 1.0.0
Release: 1
License: None
Group: REDHAWK/Waveforms
Source: %{name}-%{version}.tar.gz
# Require the controller whose SPD is referenced
Requires: MessageProducer
# Require each referenced component
Requires: MessageProducer
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}

%description

%prep
%setup

%install
%__rm -rf $RPM_BUILD_ROOT
%__mkdir_p "$RPM_BUILD_ROOT%{_prefix}/dom/waveforms/MPWaveform"
%__install -m 644 MPWaveform.sad.xml $RPM_BUILD_ROOT%{_prefix}/dom/waveforms/MPWaveform/MPWaveform.sad.xml

%files
%defattr(-,redhawk,redhawk)
%dir %{_prefix}/dom/waveforms/MPWaveform
%{_prefix}/dom/waveforms/MPWaveform/MPWaveform.sad.xml
