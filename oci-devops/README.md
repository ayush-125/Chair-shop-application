# OCI DevOps deployment

This configuration deploys the app to Oracle Kubernetes Engine (OKE) using images
stored in Oracle Container Registry (OCIR). SQLite requires one API replica because
the database lives on a single `ReadWriteOnce` persistent volume.

## One-time OCI DevOps setup

1. Create an OCIR repository for `chair-shop-api` and `chair-shop-web`.
2. Create an OKE cluster and OCI DevOps project.
3. Add a managed build stage, using `oci-devops/build_spec.yaml`.
4. Replace `<region-key>.ocir.io/<tenancy-namespace>` in the build spec with your OCIR registry endpoint.
5. Add the two Docker image output artifacts to a **Deliver Artifacts** stage.
6. Create a deployment pipeline with an OKE environment and add `app.yaml` as a Kubernetes manifest artifact.
7. Enable argument substitution for the manifest and supply `OCIR_REGISTRY` and `IMAGE_TAG` (`IMAGE_TAG` should match the build run tag).

OCI DevOps applies Kubernetes manifests server-side. It needs IAM policies for the
build pipeline to push to OCIR and for the deployment pipeline to access the OKE cluster.
